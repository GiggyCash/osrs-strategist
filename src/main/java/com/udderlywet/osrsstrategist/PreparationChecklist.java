package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class PreparationChecklist
{
    @Getter
    private final String title;
    private final List<PreparationItem> items = new ArrayList<>();

    public void add(PreparationItem item) { items.add(item); }
    public List<PreparationItem> getItems() { return Collections.unmodifiableList(items); }
    public boolean ready() { return items.stream().allMatch(PreparationItem::ready); }
}
