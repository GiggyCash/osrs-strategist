#!/usr/bin/env bash
set -euo pipefail

# Development-time only: derive the offline Compass STASH evidence resource
# from the pinned RuneLite source artefact. This script performs no game-time
# networking and never runs from the plugin.
source_jar="${1:-}"
if [[ -z "$source_jar" || ! -f "$source_jar" ]]; then
  echo "usage: $0 /path/to/client-VERSION-sources.jar" >&2
  exit 2
fi

task_tmp="$(mktemp -d)"
trap 'rm -rf -- "$task_tmp"' EXIT
unzip -p "$source_jar" \
  net/runelite/client/plugins/cluescrolls/clues/EmoteClue.java \
  > "$task_tmp/EmoteClue.java"
unzip -p "$source_jar" \
  net/runelite/client/plugins/cluescrolls/clues/emote/STASHUnit.java \
  > "$task_tmp/STASHUnit.java"

perl -0777 - "$task_tmp/EmoteClue.java" "$task_tmp/STASHUnit.java" <<'PERL'
use strict;
use warnings;
my ($emote_path, $stash_path) = @ARGV;
open my $emote_file, '<', $emote_path or die $!;
open my $stash_file, '<', $stash_path or die $!;
local $/;
my $source = <$emote_file>;
my $stash_source = <$stash_file>;
my @unit_order = ($stash_source =~ /^\s*([A-Z_][A-Z0-9_]*)\(ObjectID\.HH_/mg);
my %is_unit = map { $_ => 1 } @unit_order;
my %evidence;

while ($source =~ /new EmoteClue\(/g)
{
    my $start = $-[0];
    my $cursor = $+[0];
    my ($depth, $quoted, $escaped) = (1, 0, 0);
    for (; $cursor < length($source) && $depth; $cursor++)
    {
        my $char = substr($source, $cursor, 1);
        if ($quoted)
        {
            if ($escaped) { $escaped = 0; }
            elsif ($char eq '\\') { $escaped = 1; }
            elsif ($char eq '"') { $quoted = 0; }
        }
        else
        {
            if ($char eq '"') { $quoted = 1; }
            elsif ($char eq '(') { $depth++; }
            elsif ($char eq ')') { $depth--; }
        }
    }
    my $call = substr($source, $start, $cursor - $start);
    pos($source) = $cursor;
    my ($unit) = grep { $call =~ /\b\Q$_\E\b/ } @unit_order;
    next unless $unit;

    my @strings;
    while ($call =~ /"((?:\\.|[^"\\])*)"/g)
    {
        push @strings, $1;
        last if @strings == 2;
    }
    next unless @strings == 2;
    for (@strings)
    {
        s/\\"/"/g;
        s/\\t/ /g;
        s/[\r\n]+/ /g;
    }
    # RuneLite temporarily carries two equivalent wordings for one Iorwerth
    # clue. The STASH identity and equipment evidence are the same, so keep the
    # first deterministically.
    $evidence{$unit} //= [$strings[1], $strings[0]];
}

print "# RuneLite STASH identity\tlocation\tauthoritative emote/equipment evidence\n";
for my $unit (@unit_order)
{
    die "Missing EmoteClue evidence for $unit\n" unless exists $evidence{$unit};
    print join("\t", $unit, @{$evidence{$unit}}), "\n";
}
die "Expected 119 STASH identities, found " . scalar(@unit_order) . "\n"
    unless @unit_order == 119;
PERL
