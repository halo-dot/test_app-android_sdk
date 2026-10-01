#!/usr/bin/env bash
#
# Moves the test_app/ folder from the master branch of
# halo-dot/test_app-android_sdk into the main branch of
# halo-dot/halo_sdk_plugins under test_apps/android_sdk, keeping the full
# commit history of that folder.
#
# Only SOURCE_PATH is kept from the source repo; everything else (e.g.
# multi_activity_template/) and commits that never touched it are dropped.
# Its contents are placed under TARGET_SUBDIR in the target repo so they
# cannot collide with existing files. History is rewritten with
# git-filter-repo so `git log -- <subdir>` and `git blame` work on the moved
# files.
#
# Safe to re-run: already-imported commits are recognised and only new source
# commits are merged. TARGET_SUBDIR is then reset to exactly match the source,
# removing any files in it that don't exist in the source.
#
# Usage:
#   scripts/migrate_test_app_to_halo_sdk_plugins.sh              # merge and push
#   DRY_RUN=1 scripts/migrate_test_app_to_halo_sdk_plugins.sh    # merge locally, don't push
#   TARGET_SUBDIR= scripts/migrate_test_app_to_halo_sdk_plugins.sh   # merge into repo root
#   SOURCE_PATH= scripts/migrate_test_app_to_halo_sdk_plugins.sh     # move the whole source repo
#
# Requirements: git, git-filter-repo (pip install git-filter-repo) when
# SOURCE_PATH or TARGET_SUBDIR is non-empty, and push access to the target repo.

set -euo pipefail

SOURCE_URL="${SOURCE_URL:-https://github.com/halo-dot/test_app-android_sdk.git}"
SOURCE_BRANCH="${SOURCE_BRANCH:-master}"
SOURCE_PATH="${SOURCE_PATH-test_app}"
TARGET_URL="${TARGET_URL:-https://github.com/halo-dot/halo_sdk_plugins.git}"
TARGET_BRANCH="${TARGET_BRANCH:-main}"
TARGET_SUBDIR="${TARGET_SUBDIR-test_apps/android_sdk}"
DRY_RUN="${DRY_RUN:-0}"
WORK_DIR="${WORK_DIR:-$(mktemp -d -t halo-migrate-XXXXXX)}"

STEP=0
# Prints a numbered step heading.
step() { STEP=$((STEP + 1)); printf '\n==> Step %d: %s\n' "$STEP" "$*"; }
# Echoes a command (shell-quoted) and then runs it.
run() { { printf '+'; printf ' %q' "$@"; printf '\n'; } >&2; "$@"; }

if [[ -n "$SOURCE_PATH$TARGET_SUBDIR" ]] && ! git filter-repo --version >/dev/null 2>&1; then
    echo "git-filter-repo is required (pip install git-filter-repo)" >&2
    exit 1
fi

step "Preparing working directory $WORK_DIR"
SOURCE_DIR="$WORK_DIR/source"
TARGET_DIR="$WORK_DIR/target"
echo "Source: $SOURCE_URL ($SOURCE_BRANCH) folder '${SOURCE_PATH:-<repo root>}'"
echo "Target: $TARGET_URL ($TARGET_BRANCH) under '${TARGET_SUBDIR:-<repo root>}'"

step "Cloning $SOURCE_URL ($SOURCE_BRANCH)"
run git clone --branch "$SOURCE_BRANCH" --single-branch --no-tags "$SOURCE_URL" "$SOURCE_DIR"

if [[ -n "$SOURCE_PATH" && -n "$TARGET_SUBDIR" ]]; then
    step "Rewriting source history: keeping only '$SOURCE_PATH/', moved to '$TARGET_SUBDIR/'"
    run git -C "$SOURCE_DIR" filter-repo --path "$SOURCE_PATH/" \
        --path-rename "$SOURCE_PATH/:$TARGET_SUBDIR/" --force
elif [[ -n "$SOURCE_PATH" ]]; then
    step "Rewriting source history: keeping only '$SOURCE_PATH/', moved to repo root"
    run git -C "$SOURCE_DIR" filter-repo --subdirectory-filter "$SOURCE_PATH" --force
elif [[ -n "$TARGET_SUBDIR" ]]; then
    step "Rewriting source history into subdirectory '$TARGET_SUBDIR/'"
    run git -C "$SOURCE_DIR" filter-repo --to-subdirectory-filter "$TARGET_SUBDIR" --force
fi

step "Checking branches on $TARGET_URL"
TARGET_HEADS="$(run git ls-remote --heads "$TARGET_URL")" || {
    echo "Cannot reach $TARGET_URL (check the URL and your access)" >&2
    exit 1
}

if grep -q "refs/heads/$TARGET_BRANCH\$" <<<"$TARGET_HEADS"; then
    step "Cloning $TARGET_URL ($TARGET_BRANCH)"
    run git clone --branch "$TARGET_BRANCH" --single-branch "$TARGET_URL" "$TARGET_DIR"

    step "Fetching rewritten source history into the target clone"
    run git -C "$TARGET_DIR" fetch "$SOURCE_DIR" "$SOURCE_BRANCH:migrated-test-app"

    # git-filter-repo is deterministic, so on a re-run the already-imported
    # commits keep their IDs and only new source commits need merging.
    if run git -C "$TARGET_DIR" merge-base --is-ancestor migrated-test-app HEAD &&
        { [[ -z "$TARGET_SUBDIR" ]] ||
            run git -C "$TARGET_DIR" diff --quiet migrated-test-app HEAD -- "$TARGET_SUBDIR"; }; then
        step "Nothing to do: $TARGET_BRANCH already has the latest source history"
        exit 0
    fi

    step "Merging source history into $TARGET_BRANCH"
    # Conflicts can only be inside TARGET_SUBDIR (the source touches nothing
    # else), and they're resolved below by taking the source's version.
    run git -C "$TARGET_DIR" merge --allow-unrelated-histories --no-ff --no-commit \
        migrated-test-app || true

    if [[ -n "$TARGET_SUBDIR" ]]; then
        step "Cleaning '$TARGET_SUBDIR/' so it matches the source exactly"
        # Drops leftovers (e.g. files from an earlier import or edits made
        # directly in the target) and restores the source's files.
        run git -C "$TARGET_DIR" rm -r -q -f --ignore-unmatch -- "$TARGET_SUBDIR"
        run git -C "$TARGET_DIR" checkout migrated-test-app -- "$TARGET_SUBDIR"
    fi

    run git -C "$TARGET_DIR" commit --no-edit \
        -m "Import test_app-android_sdk ${SOURCE_PATH:+$SOURCE_PATH/ }($SOURCE_BRANCH) with history"
    run git -C "$TARGET_DIR" diff --stat HEAD^1 HEAD
    PUSH_DIR="$TARGET_DIR"
else
    # Target branch doesn't exist yet (e.g. empty repo): push source history as-is.
    step "$TARGET_BRANCH does not exist on target; pushing source history directly"
    PUSH_DIR="$SOURCE_DIR"
fi

if [[ "$DRY_RUN" == "1" ]]; then
    step "DRY_RUN=1: not pushing. Inspect the result in $PUSH_DIR"
    run git -C "$PUSH_DIR" log --oneline --graph -n 15
    exit 0
fi

step "Pushing to $TARGET_URL ($TARGET_BRANCH)"
run git -C "$PUSH_DIR" push "$TARGET_URL" "HEAD:refs/heads/$TARGET_BRANCH"

step "Done. Temporary files left in $WORK_DIR (safe to delete)"
