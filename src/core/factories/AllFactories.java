package core.factories;

/**
 * Loader class for factory data
 * @author nhydock
 */
public final class AllFactories {

	public static void prepare()
	{
		ItemFactory.init();
		CraftableFactory.init();
		MonsterFactory.init();
		AdjectiveFactory.init();
	}
	
}
