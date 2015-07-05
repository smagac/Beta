package com.nhydock.scenes.scene2d.ui.extras;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.SnapshotArray;

public class TableUtils {
    public static void removeTableRow(Table table, int row, final int COLUMNS) {

        SnapshotArray<Actor> children = table.getChildren();
        children.ordered = false;

        for (int i = row * COLUMNS; i < children.size - COLUMNS; i++) {
            children.swap(i, i + COLUMNS);
        }

        // Remove last row
        for (int i = 0; i < COLUMNS; i++) {
            table.removeActor(children.get(children.size - 1));
        }
    }

    public static Actor getActorFromRow(Table table, int row, final int COLUMNS, int column) {
        SnapshotArray<Actor> children = table.getChildren();
        children.ordered = false;

        return children.get((row * COLUMNS) + column);
    }
}
