#!/usr/bin/env python3
"""Run a command with application secrets loaded from a dotenv file."""

import os
import sys
from pathlib import Path
from dotenv import dotenv_values

ALLOWED_KEYS = {
    "JWT_SECRET",
    "POSTGRES_USER",
    "POSTGRES_PASSWORD",
    "REDIS_PASSWORD",
    "NEO4J_PASSWORD",
}


def main() -> None:
    if len(sys.argv) < 3:
        raise SystemExit("usage: with-dotenv.py ENV_FILE COMMAND [ARGS...]")

    env_file, *command = sys.argv[1:]
    env_path = Path(env_file).resolve()

    if not env_path.is_file():
        raise SystemExit(f"Environment file not found: {env_path}")

    parsed = dotenv_values(env_path)
    environment = os.environ.copy()

    print(f"===================Loading dotenv file: {env_path}", file=sys.stderr)

    for key in sorted(ALLOWED_KEYS):
        value = parsed.get(key)

        if value is not None:
            environment[key] = value

        status = (
            "PRESENT"
            if environment.get(key)
            else "MISSING_OR_EMPTY"
        )
        print(f"=================== {key}:::::::::::::::::::::: {status}", file=sys.stderr)

    os.execvpe(command[0], command, environment)


if __name__ == "__main__":
    main()