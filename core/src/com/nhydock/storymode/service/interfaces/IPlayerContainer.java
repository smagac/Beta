package com.nhydock.storymode.service.interfaces;

import github.nhydock.ssm.Service;

import com.badlogic.ashley.core.Entity;
import com.nhydock.storymode.datatypes.Health;
import com.nhydock.storymode.datatypes.Inventory;
import com.nhydock.storymode.datatypes.QuestTracker;

public interface IPlayerContainer extends Service {

    public Inventory getInventory();

    public QuestTracker getQuestTracker();

    public Entity getPlayer();
    
    public Health getAilments();

    public void rest();

    public void recover();

    public int[] getTimeElapsed();

    public String getFullTime();

    public String getGender();

    public String getWorship();
    
    public int getDaysElapsed();

    public SaveSummary summary(int slot);

    public int slots();

    public void save(int slot);

    public void load(int slot);

    public static class SaveSummary {
        public String gender;
        public String time;
        public String progress;
        public String date;
        public int diff;
        public boolean hardcore;
    }

    public void init(int difficulty, boolean gender, boolean hardcore);

    public boolean isPrepared();
    
    public boolean isHardcore();

    public void updateTime(float delta);
}
