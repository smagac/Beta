package github.nhydock;

public class CollectionUtils {

    /**
     * Pick a item from a list
     * @param items
     * @return
     */
    @SafeVarargs
    public static <T> T randomChoice(T... items) {
        return items[randomIndex(items)];
    }
    
    /**
     * Pick a random index from a list of items
     * @param items - the collection of data
     * @return a index value between 0 and n-1
     */
    @SafeVarargs
    public static <T> int randomIndex(T... items) {
        int picked = (int)(Math.random() * items.length);
        return picked;
    }
}
