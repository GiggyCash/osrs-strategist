package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PreparationChecklist
{
    private final String title;
    private final List<PreparationItem> items = new ArrayList<>();

    public PreparationChecklist(String title) { this.title = title; }
    public void add(PreparationItem item) { items.add(item); }
    public String getTitle() { return title; }
    public List<PreparationItem> getItems() { return Collections.unmodifiableList(items); }
    public boolean ready() { return items.stream().allMatch(PreparationItem::ready); }
}
