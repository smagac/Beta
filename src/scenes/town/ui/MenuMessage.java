package scenes.town.ui;

/**
 * Message types for the main state
 * 
 * @author nhydock
 *
 */
interface MenuMessage {
    static final int Sleep = 0;
    static final int Explore = 1;
    static final int Craft = 2;
    static final int Quest = 3;
    static final int Save = 4;

    static final int Make = 1;
    static final int Accept = 1;
    static final int Refresh = 2;

    static final int Random = 2;
    static final int DailyDungeon = 3;

    static final int CancelDownload = 0;
}