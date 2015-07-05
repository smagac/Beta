package com.nhydock.storymode.datatypes.quests.info;
public class GatherInfo {
    public final String itemName;
    public final int itemCount;
    
    public GatherInfo(String name, int count)
    {
        itemName = name;
        itemCount = count;
    }
}