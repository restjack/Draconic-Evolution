"""Build both pinned library ports and then forward the requested tasks to Draconic Evolution.

Usage: python gradle/build-port.py [Gradle tasks/options, default: build].
Set DE_BUILD_ROOT to select a directory for checkouts, caches, jars and development runs.
Requires Python 3.8+, Git and JDK 25 on PATH. Dependency projects use their own Gradle wrappers.
"""
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import tempfile
from urllib.request import Request, urlopen


PROJECT = Path(__file__).resolve().parents[1]
STAGE = Path(os.environ.get("DE_BUILD_ROOT", PROJECT / ".gradle" / "port-work")).resolve()
LIBRARIES = (
    ("CodeChickenLib", "d1a10f75ce09af2ffe23783d860ee3e282a72d91",
     "codechickenlib-4.7.0+26.1.2-port.2.jar"),
    ("BrandonsCore", "6ca5911187d8c071d0596b668a2bd7d144b7cdc2",
     "BrandonsCore-3.2.1+26.1.2-port.5.jar"),
)


def git(repo, *args, capture=False):
    return subprocess.run(
        ["git", "-C", str(repo), *args], check=True, text=True,
        stdout=subprocess.PIPE if capture else None
    ).stdout


def checkout(name, revision):
    patch = PROJECT / "gradle" / (name.lower() + "-26.1.2.patch")
    source = STAGE / "sources" / name
    source.mkdir(parents=True, exist_ok=True)
    if not (source / ".git").is_dir():
        git(source, "init", "--quiet")
    try:
        head = git(source, "rev-parse", "--verify", "--quiet", "HEAD", capture=True).strip()
    except subprocess.CalledProcessError:
        head = None
    if head != revision:
        url = f"https://github.com/MehanikTMYT/{name}.git"
        git(source, "fetch", "--depth=1", url, revision)
        git(source, "checkout", "--detach", revision)
    # Apply the tracked upstream fixes exactly once; reject conflicting local edits.
    if patch.is_file():
        reversed_check = subprocess.run(
            ["git", "-C", str(source), "apply", "--reverse", "--check", str(patch)],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
        )
        if reversed_check.returncode:
            git(source, "apply", "--index", str(patch))
    changes = git(source, "diff", "--name-only", capture=True)
    if changes:
        raise RuntimeError(f"Tracked files in the dependency checkout have local edits: {source}")
    return source


def gradle(directory, *args):
    wrapper = "gradlew.bat" if os.name == "nt" else "gradlew"
    subprocess.run([str(directory / wrapper), "--init-script", str(PROJECT / "gradle" / "neoforge-mirror.gradle"),
                    *args], cwd=directory, check=True)



def write_if_changed(target, data):
    if target.is_file() and target.read_bytes() == data:
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=target.parent, delete=False) as output:
        output.write(data)
    temp_path = Path(output.name)
    try:
        os.replace(temp_path, target)
    finally:
        temp_path.unlink(missing_ok=True)


def prepare_maven():
    # These publisher-hosted files occasionally time out in Gradle's HTTP client.
    # Use immutable checksums and normal Maven layout so the same coordinates work offline.
    repositories = {
        "net/neoforged/": "https://maven.neoforged.net/releases/",
        "cc/tweaked/": "https://maven.squiddev.cc/",
        "top/theillusivec4/curios/": "https://maven.theillusivec4.top/",
    }
    for line in (PROJECT / "gradle" / "port-maven.sha256").read_text().splitlines():
        checksum, relative = line.split()
        target = STAGE / "mirror" / relative
        if target.is_file() and hashlib.sha256(target.read_bytes()).hexdigest() == checksum:
            continue
        origin = next(value for prefix, value in repositories.items() if relative.startswith(prefix))
        request = Request(origin + relative, headers={"User-Agent": "Draconic-Evolution-port-build"})
        with urlopen(request, timeout=60) as response:
            data = response.read()
        if hashlib.sha256(data).hexdigest() != checksum:
            raise ValueError(f"Unexpected SHA256 from publisher for {relative}")
        write_if_changed(target, data)


def main():
    os.environ["DE_BUILD_ROOT"] = str(STAGE)
    os.environ.setdefault("GRADLE_USER_HOME", str(STAGE / "gradle-home"))
    prepare_maven()
    libraries = STAGE / "deps"
    libraries.mkdir(parents=True, exist_ok=True)
    # The Brandon's Core build consumes the adjacent CodeChickenLib checkout's jar.
    for name, revision, filename in LIBRARIES:
        source = checkout(name, revision)
        gradle(source, "jar")
        write_if_changed(libraries / filename, (source / "build" / "libs" / filename).read_bytes())
    gradle(PROJECT, "--project-cache-dir", str(STAGE / "Draconic-Evolution" / "project-cache"),
           *(sys.argv[1:] or ["build"]))


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        sys.exit(error.returncode)
