#!/usr/bin/env python3
import argparse
import psycopg2
from pathlib import Path
from textwrap import dedent

def read_sql(path: Path) -> str:
    text = path.read_text(encoding="utf-8")
    return "\n".join(
        line for line in text.splitlines() if not line.lstrip().startswith("\\")
    )


def fetch_dv_ids(conn, find_dv_ids_sql: str) -> list[int]:
    with conn.cursor() as cur:
        cur.execute(find_dv_ids_sql)
        rows = cur.fetchall()

    # Query returns dv_id as first selected column in your file.
    return [int(row[0]) for row in rows]


def fetch_dataset_info(conn, datasetversion_id: int):
    dataset_query = """
                    SELECT dso.protocol, dso.authority, dso.identifier, dv.versionnumber, dv.minorversionnumber
                    FROM datasetversion dv
                             JOIN dvobject dso ON dso.id = dv.dataset_id
                    WHERE dv.id = %s \
                    """
    with conn.cursor() as cur:
        cur.execute(dataset_query, (datasetversion_id,))
        return cur.fetchone()
    return None


def run_find_duplicates(conn, find_duplicates_sql: str):
    last_dv_id = None
    last_info = ("", "", "", "", "")

    with conn.cursor() as cur:
        cur.execute(find_duplicates_sql)
        cols = [d[0] for d in cur.description]

        extra_cols = ["protocol", "authority", "dataset_id", "versionnumber", "minorversionnumber"]
        print("\t".join(cols + extra_cols))

        for row in cur:
            dv_id = int(row[0])  # datasetversion_id

            if dv_id != last_dv_id:
                fetched = fetch_dataset_info(conn, dv_id)
                last_info = fetched if fetched is not None else ("", "", "", "", "")
                last_dv_id = dv_id

            print("\t".join("" if v is None else str(v) for v in (tuple(row) + tuple(last_info))))


def main():
    class RawDefaultsFormatter(
        argparse.ArgumentDefaultsHelpFormatter,
        argparse.RawDescriptionHelpFormatter,
    ):
        pass

    parser = argparse.ArgumentParser(
        description=dedent("""
            Execute as owner of dvndb.
            
            `find_duplicates.sql` is executed for dv_ids returned by `find_dv_ids.sql`.
            `find_dv_ids.sql` returns the latest version per dataset.
        """),
        formatter_class=RawDefaultsFormatter,
    )
    parser.add_argument("--min-id", type=int, default=0, help="first dataset-version-id examined by `find_dv_ids.sql`")
    parser.add_argument("--nr-of-ids", type=int, default=50, help="number of ID's returned by `find_dv_ids.sql`")
    args = parser.parse_args()
    conn_kwargs = {"dbname": 'dvndb'}

    script_dir = Path(__file__).resolve().parent

    dup_sql_raw = read_sql(script_dir / "find_duplicates.sql")

    dv_sql = read_sql(script_dir / "find_dv_ids.sql")
    dv_sql = dv_sql.replace(":min_id", str(args.min_id))
    dv_sql = dv_sql.replace(":nr_of_ids", str(args.nr_of_ids))

    try:
        with psycopg2.connect(**conn_kwargs) as conn:
            dv_ids = fetch_dv_ids(conn, dv_sql)

            if not dv_ids:
                print("No dv_id values returned by find_dv_ids.sql")
                return

            ids_csv = ",".join(str(i) for i in dv_ids)
            print(f"dataset version ids: {ids_csv}")
            run_find_duplicates(conn, dup_sql_raw.replace(":ids", ids_csv))
    except psycopg2.OperationalError as e:
        msg = str(e)
        if "no password supplied" in msg.lower():
            parser.print_help()
            raise SystemExit(2)
        print(f"Database connection failed: {e}")
        raise SystemExit(1)

if __name__ == "__main__":
    main()
