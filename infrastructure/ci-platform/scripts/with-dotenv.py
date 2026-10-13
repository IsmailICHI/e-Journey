#!/usr/bin/env python3
"""Run a command with application secrets loaded from a dotenv file."""

import os
import sys
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
    parsed = dotenv_values(env_file)
    environment = os.environ.copy()
    for key in ALLOWED_KEYS:
        value = parsed.get(key)
        if value is not None:
            environment[key] = value

    os.execvpe(command[0], command, environment)


if __name__ == "__main__":
    main()
