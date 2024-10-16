#!/bin/bash

python3 -m venv run_env
source run_env/bin/activate
python3 -m pip install -r requirements.txt
python3 get_session.py
rm -rf run_env