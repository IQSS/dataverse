#!/usr/bin/env python3
"""
investigate.py: Benchmark and reproduce Dataverse upload degradation on large file counts.
Reproduces Federico Yemurenko's scenario:
  - VM with 2 CPUs, 8GB RAM, local filesystem storage
  - Sequential file uploads in batches of N files with delay
  - Demonstrates how upload latency degrades from ~1s to ~30s+ due to:
    1. O(N) SQL updates: UPDATE DVOBJECT SET MODIFICATIONTIME = ... on ALL existing files per upload (O(N^2) total)
    2. Deep cloning of all FileMetadata objects in memory per upload
    3. Jakarta Bean Validation with reflection on all FileMetadata objects per upload
    4. Async Solr delete and re-index of all N files per upload
"""

import argparse
import io
import json
import os
import subprocess
import sys
import time
from typing import Optional

try:
    import requests
except ImportError:
    print("Error: 'requests' library is required. Install with: pip install requests")
    sys.exit(1)


def parse_args():
    parser = argparse.ArgumentParser(description="Investigate Dataverse upload performance degradation.")
    parser.add_argument("--url", default="http://localhost:8080", help="Dataverse base URL (e.g. http://localhost:8080 or http://localhost:8085)")
    parser.add_argument("--api-key", default=None, help="Dataverse API Key (default: looks up dataverseAdmin or prompt)")
    parser.add_argument("--total-files", type=int, default=100, help="Total files to upload in this run")
    parser.add_argument("--batch-size", type=int, default=20, help="Batch size before delay")
    parser.add_argument("--batch-delay", type=float, default=2.0, help="Delay in seconds between batches")
    parser.add_argument("--dataset-id", type=int, default=None, help="Existing Dataset ID (or creates a new dataset)")
    parser.add_argument("--postgres-container", default=None, help="Postgres container name to inspect SQL logs (e.g. dev_postgres or investigate_postgres)")
    return parser.parse_args()


def get_api_key(args) -> str:
    if args.api_key:
        return args.api_key
    env_key = os.environ.get("DATAVERSE_API_KEY")
    if env_key:
        return env_key
    # Attempt to query local postgres if container is running
    containers = ["dev_postgres", "investigate_postgres"]
    if args.postgres_container:
        containers = [args.postgres_container] + containers
    for container in containers:
        try:
            cmd = [
                "docker", "exec", container,
                "psql", "-U", "dataverse", "-d", "dataverse", "-t", "-A", "-c",
                "SELECT t.tokenstring FROM authenticateduser u JOIN apitoken t ON u.id = t.authenticateduser_id WHERE u.superuser = true LIMIT 1;"
            ]
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
            token = res.stdout.strip()
            if token and len(token) > 10:
                print(f"[+] Found superuser API token from {container}: {token}")
                return token
        except Exception:
            continue
    raise RuntimeError("API key not found. Please specify via --api-key or DATAVERSE_API_KEY env var.")


def create_dataset(base_url: str, api_key: str) -> int:
    print("[*] Creating a new test dataset...")
    url = f"{base_url.rstrip('/')}/api/dataverses/root/datasets"
    headers = {"X-Dataverse-key": api_key, "Content-Type": "application/json"}
    payload = {
        "datasetVersion": {
            "metadataBlocks": {
                "citation": {
                    "fields": [
                        {
                            "typeName": "title",
                            "multiple": False,
                            "typeClass": "primitive",
                            "value": f"Benchmark Large Upload {int(time.time())}"
                        },
                        {
                            "typeName": "author",
                            "multiple": True,
                            "typeClass": "compound",
                            "value": [{"authorName": {"typeName": "authorName", "multiple": False, "typeClass": "primitive", "value": "Benchmark, Tester"}}]
                        },
                        {
                            "typeName": "datasetContact",
                            "multiple": True,
                            "typeClass": "compound",
                            "value": [
                                {
                                    "datasetContactEmail": {"typeName": "datasetContactEmail", "multiple": False, "typeClass": "primitive", "value": "test@example.com"},
                                    "datasetContactName": {"typeName": "datasetContactName", "multiple": False, "typeClass": "primitive", "value": "Tester"}
                                }
                            ]
                        },
                        {
                            "typeName": "dsDescription",
                            "multiple": True,
                            "typeClass": "compound",
                            "value": [{"dsDescriptionValue": {"typeName": "dsDescriptionValue", "multiple": False, "typeClass": "primitive", "value": "Test for bulk upload degradation."}}]
                        },
                        {
                            "typeName": "subject",
                            "multiple": True,
                            "typeClass": "controlledVocabulary",
                            "value": ["Other"]
                        }
                    ]
                }
            }
        }
    }
    r = requests.post(url, headers=headers, json=payload, timeout=30)
    r.raise_for_status()
    data = r.json()
    ds_id = data["data"]["id"]
    pid = data["data"]["persistentId"]
    print(f"[+] Created test dataset ID: {ds_id} (PID: {pid})")
    return ds_id


def get_postgres_mod_time_updates(container: str) -> Optional[int]:
    """Count how many UPDATE DVOBJECT SET MODIFICATIONTIME queries have been logged."""
    try:
        cmd = f"docker logs {container} 2>&1 | grep -c 'UPDATE DVOBJECT SET MODIFICATIONTIME'"
        res = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=5)
        out = res.stdout.strip()
        if out.isdigit():
            return int(out)
    except Exception:
        pass
    return None


def run_benchmark(base_url: str, api_key: str, dataset_id: int, total_files: int, batch_size: int, batch_delay: float, pg_container: Optional[str]):
    url = f"{base_url.rstrip('/')}/api/datasets/{dataset_id}/add"
    headers = {"X-Dataverse-key": api_key}

    print(f"\n{'='*75}")
    print(f"Starting Upload Benchmark: {total_files} files in batches of {batch_size}")
    print(f"Target URL: {url}")
    print(f"{'='*75}\n")

    latencies = []
    batch_latencies = []
    initial_updates = get_postgres_mod_time_updates(pg_container) if pg_container else None

    # Minimal 1x1 transparent GIF bytes
    gif_bytes = b"GIF89a\x01\x00\x01\x00\x80\x00\x00\xff\xff\xff\x00\x00\x00!\xf9\x04\x01\x00\x00\x00\x00,\x00\x00\x00\x00\x01\x00\x01\x00\x00\x02\x02D\x01\x00;"

    total_start = time.time()
    for idx in range(1, total_files + 1):
        is_gif = (idx % 2 == 0)
        filename = f"file_{idx:05d}.gif" if is_gif else f"file_{idx:05d}.txt"
        file_content = gif_bytes if is_gif else f"Dataverse file content payload for file index {idx}\n".encode("utf-8")
        mime = "image/gif" if is_gif else "text/plain"

        files = {
            "file": (filename, file_content, mime),
            "jsonData": (None, json.dumps({"description": f"Benchmark item {idx}"}), "application/json")
        }

        t0 = time.time()
        res = requests.post(url, headers=headers, files=files, timeout=120)
        t1 = time.time()

        if res.status_code not in (200, 201):
            print(f"[!] Error on file #{idx}: HTTP {res.status_code} - {res.text}")
            continue

        latency = t1 - t0
        latencies.append(latency)
        batch_latencies.append(latency)

        if idx % 10 == 0 or idx == total_files:
            avg_recent = sum(batch_latencies) / len(batch_latencies)
            print(f"  [#{idx:04d}/{total_files:04d}] Latency: {latency:.3f}s | Avg (last {len(batch_latencies)}): {avg_recent:.3f}s")
            batch_latencies = []

        if idx % batch_size == 0 and idx < total_files:
            print(f"  [*] Completed batch of {batch_size}. Pausing {batch_delay}s (replicating DVUploader delay)...")
            time.sleep(batch_delay)

    total_duration = time.time() - total_start
    final_updates = get_postgres_mod_time_updates(pg_container) if pg_container else None

    print(f"\n{'='*75}")
    print("BENCHMARK SUMMARY & METRICS")
    print(f"{'='*75}")
    print(f"Total files uploaded:   {len(latencies)}")
    print(f"Total time taken:       {total_duration:.2f} seconds ({total_duration/60:.2f} minutes)")
    print(f"Min latency:            {min(latencies):.3f}s")
    print(f"Max latency:            {max(latencies):.3f}s")
    print(f"First 10 files avg:     {sum(latencies[:10])/min(len(latencies), 10):.3f}s")
    print(f"Last 10 files avg:      {sum(latencies[-10:])/min(len(latencies), 10):.3f}s")
    
    if initial_updates is not None and final_updates is not None:
        delta_updates = final_updates - initial_updates
        print(f"SQL UPDATEs on DVOBJECT (MODIFICATIONTIME): {delta_updates} queries")
        print(f"Theoretical O(N^2/2) updates for {len(latencies)} files: {len(latencies) * (len(latencies) + 1) // 2}")

    print("\n--- ANALYSIS ---")
    print("Notice how upload latency scales with the existing number of files N:")
    print("  1. In UpdateDatasetVersionCommand.java (line 164):")
    print("     for (DataFile dataFile : theDataset.getFiles()) { dataFile.setModificationTime(getTimestamp()); }")
    print("     -> Modifies modificationTime on EVERY file in the dataset version.")
    print("     -> On JPA flush, Postgres executes N UPDATE queries per single file upload.")
    print("     -> For 14,000 files: exactly 14,000 UPDATE queries PER FILE uploaded.")
    print("  2. In DatasetVersion.java: cloneDatasetVersion() deeply copies all N FileMetadata objects in RAM.")
    print("  3. In DatasetVersion.java: validate() invokes bean validation reflection on all N FileMetadata objects.")
    print("  4. In IndexServiceBean.java: Solr queries all N files, deletes them, and re-indexes all N files.")
    print(f"{'='*75}\n")


def main():
    args = parse_args()
    api_key = get_api_key(args)
    dataset_id = args.dataset_id
    if not dataset_id:
        dataset_id = create_dataset(args.url, api_key)
    
    pg_container = args.postgres_container or ("investigate_postgres" if "8085" in args.url else "dev_postgres")
    run_benchmark(
        base_url=args.url,
        api_key=api_key,
        dataset_id=dataset_id,
        total_files=args.total_files,
        batch_size=args.batch_size,
        batch_delay=args.batch_delay,
        pg_container=pg_container
    )


if __name__ == "__main__":
    main()
