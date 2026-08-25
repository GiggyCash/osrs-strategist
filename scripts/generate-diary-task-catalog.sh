#!/usr/bin/env bash
set -euo pipefail

# Development-time only: generate the offline task/prerequisite catalogue from
# RuneLite's pinned Achievement Diary source definitions.
source_jar="${1:-}"
if [[ -z "$source_jar" || ! -f "$source_jar" ]]; then
  echo "usage: $0 /path/to/client-VERSION-sources.jar" >&2
  exit 2
fi

task_tmp="$(mktemp -d)"
trap 'rm -rf -- "$task_tmp"' EXIT
for file in Ardougne Desert Falador Fremennik Kandarin Karamja Kourend Lumbridge Morytania Varrock Western Wilderness; do
  unzip -p "$source_jar" \
    "net/runelite/client/plugins/achievementdiary/diaries/${file}DiaryRequirement.java" \
    > "$task_tmp/${file}.java"
done

perl -0777 - "$task_tmp" <<'PERL'
use strict;
use warnings;
my ($directory) = @ARGV;
my @regions = (
    ['Ardougne', 'Ardougne'], ['Desert', 'Desert'], ['Falador', 'Falador'],
    ['Fremennik', 'Fremennik'], ['Kandarin', 'Kandarin'], ['Karamja', 'Karamja'],
    ['Kourend', 'Kourend & Kebos'], ['Lumbridge', 'Lumbridge & Draynor'],
    ['Morytania', 'Morytania'], ['Varrock', 'Varrock'],
    ['Western', 'Western Provinces'], ['Wilderness', 'Wilderness']);
print "# region\ttier\ttask\tRuneLite requirement expression\n";
my $count = 0;
for my $entry (@regions)
{
    my ($file, $region) = @$entry;
    open my $source_file, '<', "$directory/$file.java" or die $!;
    local $/;
    my $source = <$source_file>;
    while ($source =~ /\badd\(/g)
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
        my $prefix = substr($source, 0, $start);
        my @tiers = ($prefix =~ m{//\s*(EASY|MEDIUM|HARD|ELITE)\s*$}mg);
        die "No tier before $file task\n" unless @tiers;
        my $tier = $tiers[-1];
        my ($task) = ($call =~ /"((?:\\.|[^"\\])*)"/);
        die "No task text in $file\n" unless defined $task;
        $task =~ s/\\"/"/g;
        my $requirements = $call;
        $requirements =~ s/^.*?"(?:\\.|[^"\\])*"\s*,?\s*//s;
        $requirements =~ s/\)\s*$//;
        $requirements =~ s/[\r\n\t]+/ /g;
        $requirements =~ s/\s+/ /g;
        $requirements =~ s/^\s+|\s+$//g;
        for ($region, $task, $requirements) { s/\t/ /g; }
        print join("\t", $region, lc($tier), $task, $requirements), "\n";
        $count++;
    }
}
die "Diary generator produced no tasks\n" unless $count;
PERL
