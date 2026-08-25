#!/usr/bin/env bash
set -euo pipefail

# Development-time only: derive canonical Slayer assignment identities and
# target aliases from RuneLite's pinned Task enum.
source_jar="${1:-}"
if [[ -z "$source_jar" || ! -f "$source_jar" ]]; then
  echo "usage: $0 /path/to/client-VERSION-sources.jar" >&2
  exit 2
fi

task_tmp="$(mktemp -d)"
trap 'rm -rf -- "$task_tmp"' EXIT
unzip -p "$source_jar" net/runelite/client/plugins/slayer/Task.java \
  > "$task_tmp/Task.java"

perl -0777 - "$task_tmp/Task.java" <<'PERL'
use strict;
use warnings;
my ($path) = @ARGV;
open my $file, '<', $path or die $!;
local $/;
my $source = <$file>;
my ($body) = ($source =~ /enum Task\s*\{(.*?)\/\/<\/editor-fold>/s);
die "Unable to find RuneLite Slayer Task enum\n" unless defined $body;
print "# RuneLite enum identity\tcanonical assignment\ttarget aliases\n";
my $count = 0;
while ($body =~ /^\s*([A-Z][A-Z0-9_]*)\(/mg)
{
    my $identity = $1;
    my $start = $-[0];
    my $cursor = $+[0];
    my ($depth, $quoted, $escaped) = (1, 0, 0);
    for (; $cursor < length($body) && $depth; $cursor++)
    {
        my $char = substr($body, $cursor, 1);
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
    my $call = substr($body, $start, $cursor - $start);
    pos($body) = $cursor;
    my @strings = ($call =~ /"((?:\\.|[^"\\])*)"/g);
    die "Missing canonical name for $identity\n" unless @strings;
    for (@strings) { s/\t/ /g; s/[\r\n]+/ /g; }
    my $name = shift @strings;
    my @fields = ($identity, $name);
    push @fields, join("|", @strings) if @strings;
    print join("\t", @fields), "\n";
    $count++;
}
die "Expected 151 Slayer identities, found $count\n" unless $count == 151;
PERL
