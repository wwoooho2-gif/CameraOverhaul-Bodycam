#!/usr/bin/env python3
"""
CameraOverhaul checker / fixer.

By default this only REPORTS problems. Nothing is changed unless you pass a fix flag,
and every change is backed up first (to <minecraft>/cameraoverhaul_backup/).

Examples:
  python3 fix_cameraoverhaul.py
  python3 fix_cameraoverhaul.py --mc-dir ~/.local/share/PrismLauncher/instances/MyPack/minecraft
  python3 fix_cameraoverhaul.py --loader fabric --fix-jars
  python3 fix_cameraoverhaul.py --reset-config
"""

import argparse
import math
import os
import re
import shutil
import sys
import time
from pathlib import Path

try:
    import tomllib  # Python 3.11+
except ModuleNotFoundError:
    tomllib = None

MOD_NAME = "cameraoverhaul"
LOG_HINTS = re.compile(
    r"(cameraoverhaul|mixin apply failed|incompatible mod set|missing or unsupported mandatory dependencies"
    r"|requires .* of (fabric|cloth|minecraft)|could not execute entrypoint|InvalidInjectionException"
    r"|NoSuchMethodError|NoSuchFieldError|ClassNotFoundException)",
    re.IGNORECASE,
)
ERROR_WORDS = re.compile(r"(ERROR|FATAL|Exception|Error:|failed)", re.IGNORECASE)


def default_mc_dir() -> Path:
    if sys.platform.startswith("win"):
        return Path(os.environ.get("APPDATA", "~")).expanduser() / ".minecraft"
    if sys.platform == "darwin":
        return Path("~/Library/Application Support/minecraft").expanduser()
    return Path("~/.minecraft").expanduser()


def say(tag: str, msg: str) -> None:
    print(f"[{tag}] {msg}")


def backup(path: Path, backup_dir: Path) -> Path:
    backup_dir.mkdir(parents=True, exist_ok=True)
    dest = backup_dir / f"{int(time.time())}_{path.name}"
    shutil.move(str(path), str(dest))
    return dest


def find_mod_jars(mods_dir: Path):
    return sorted(p for p in mods_dir.glob("*.jar") if MOD_NAME in p.name.lower())


def loader_of(jar: Path):
    name = jar.name.lower()
    if "neoforge" in name:
        return "neoforge"
    if "fabric" in name:
        return "fabric"
    if "forge" in name:
        return "forge"
    return None


def check_jars(mods_dir: Path, loader: str | None, fix: bool, backup_dir: Path) -> int:
    problems = 0
    if not mods_dir.is_dir():
        say("FAIL", f"No mods folder at {mods_dir}")
        return 1

    jars = find_mod_jars(mods_dir)
    if not jars:
        say("FAIL", "No CameraOverhaul jar in the mods folder.")
        return 1

    say("OK", f"Found {len(jars)} CameraOverhaul jar(s):")
    for j in jars:
        print(f"       - {j.name}")

    if any("-shadow" in j.name.lower() for j in jars):
        say("WARN", "A '-shadow' jar is installed. That is an intermediate build; use the one from out/latest/.")
        problems += 1

    loaders = {loader_of(j) for j in jars} - {None}
    if len(jars) > 1:
        say("WARN", "More than one CameraOverhaul jar is installed. Two copies will conflict.")
        problems += 1

    if loader:
        wrong = [j for j in jars if loader_of(j) not in (None, loader) or "-shadow" in j.name.lower()]
        if wrong:
            problems += 1
            for j in wrong:
                say("WARN", f"{j.name} does not match loader '{loader}' (or is a -shadow jar).")
                if fix:
                    dest = backup(j, backup_dir)
                    say("FIXED", f"Moved to {dest}")
    elif len(loaders) > 1:
        say("INFO", "Jars for different loaders are present. Re-run with --loader fabric|neoforge to clean up.")

    return problems


def check_dependencies(mods_dir: Path, loader: str | None) -> int:
    if not mods_dir.is_dir():
        return 0
    names = [p.name.lower() for p in mods_dir.glob("*.jar")]
    problems = 0

    def has(token):
        return any(token in n for n in names)

    if not has("cloth-config") and not has("cloth_config") and not has("clothconfig"):
        say("FAIL", "Cloth Config is missing. CameraOverhaul needs it (the settings screen and config depend on it).")
        problems += 1
    if loader == "fabric" and not has("fabric-api") and not has("fabric_api"):
        say("FAIL", "Fabric API is missing (required on Fabric).")
        problems += 1
    if not has("modmenu") and not has("mod-menu") and not has("modmenu"):
        say("INFO", "Mod Menu not found. Optional, but without it there is no in-game settings button.")
    if problems == 0:
        say("OK", "Required dependencies look present.")
    return problems


def check_config(mc_dir: Path, reset: bool, backup_dir: Path) -> int:
    cfg = mc_dir / "config" / f"{MOD_NAME}.toml"
    if not cfg.is_file():
        say("INFO", "No config file yet (it is created the first time the game starts with the mod).")
        return 0

    problems = 0
    if tomllib is None:
        say("INFO", "Python < 3.11, skipping config parsing.")
    else:
        try:
            data = tomllib.loads(cfg.read_text(encoding="utf-8"))
        except Exception as exc:
            say("FAIL", f"Config file does not parse: {exc}")
            problems += 1
            data = {}

        for section, values in data.items():
            if not isinstance(values, dict):
                continue
            for key, value in values.items():
                if isinstance(value, float) and (not math.isfinite(value) or abs(value) > 25):
                    say("WARN", f"[{section}] {key} = {value} looks extreme and can make the camera go wild.")
                    problems += 1

        general = data.get("general", {}) if isinstance(data, dict) else {}
        if general and general.get("enabled") is False:
            say("WARN", "general.enabled = false: all camera effects are switched off.")
            problems += 1
        if general and general.get("bodycamEnabled") is False:
            say("INFO", "bodycamEnabled = false: bodycam mode is off (set it to true to use it).")

    if reset:
        dest = backup(cfg, backup_dir)
        say("FIXED", f"Config reset. Old one saved at {dest}. A fresh default is created on next launch.")
    elif problems:
        say("INFO", "Run with --reset-config to back up and regenerate the default config.")
    else:
        say("OK", "Config file looks sane.")
    return problems


def scan_log(mc_dir: Path, max_lines: int = 12) -> int:
    log = mc_dir / "logs" / "latest.log"
    if not log.is_file():
        say("INFO", "No logs/latest.log found (the game has not been run, or logs live elsewhere).")
        return 0

    hits = []
    try:
        with log.open("r", encoding="utf-8", errors="replace") as fh:
            for line in fh:
                if LOG_HINTS.search(line) and ERROR_WORDS.search(line):
                    hits.append(line.rstrip())
    except OSError as exc:
        say("FAIL", f"Could not read {log}: {exc}")
        return 1

    if not hits:
        say("OK", "No CameraOverhaul-related errors in latest.log.")
        return 0

    say("WARN", f"{len(hits)} relevant line(s) in latest.log (showing the first {min(len(hits), max_lines)}):")
    for line in hits[:max_lines]:
        print(f"       {line[:220]}")
    say("INFO", "If this is not enough to tell what is wrong, send me those lines.")
    return 1


def main() -> int:
    ap = argparse.ArgumentParser(description="Check and repair a CameraOverhaul install.")
    ap.add_argument("--mc-dir", type=Path, default=None, help="Minecraft folder (default: auto-detect).")
    ap.add_argument("--loader", choices=["fabric", "neoforge"], help="Your mod loader.")
    ap.add_argument("--fix-jars", action="store_true", help="Move wrong-loader / duplicate / shadow jars out of mods/.")
    ap.add_argument("--reset-config", action="store_true", help="Back up and remove the config so defaults regenerate.")
    args = ap.parse_args()

    mc_dir = (args.mc_dir or default_mc_dir()).expanduser()
    if not mc_dir.is_dir():
        say("FAIL", f"Minecraft folder not found: {mc_dir}\n       Pass it with --mc-dir.")
        return 2

    backup_dir = mc_dir / "cameraoverhaul_backup"
    mods_dir = mc_dir / "mods"
    print(f"Minecraft folder: {mc_dir}\n")

    if args.fix_jars and not args.loader:
        say("FAIL", "--fix-jars needs --loader fabric|neoforge so I know which jar to keep.")
        return 2

    total = 0
    total += check_jars(mods_dir, args.loader, args.fix_jars, backup_dir)
    print()
    total += check_dependencies(mods_dir, args.loader)
    print()
    total += check_config(mc_dir, args.reset_config, backup_dir)
    print()
    total += scan_log(mc_dir)
    print()

    if total:
        say("DONE", f"{total} possible problem(s) found.")
    else:
        say("DONE", "Nothing obviously wrong. If it still misbehaves, tell me what you see.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
