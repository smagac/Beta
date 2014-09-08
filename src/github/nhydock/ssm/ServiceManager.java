package github.nhydock.ssm;

import java.lang.reflect.Field;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Simple Injection based handler, allowing for setting fields
 * that are properly annotated
 * @author nhydock
 *
 */
public class ServiceManager {
	
	private static ObjectMap<Class<? extends Service>, Service> services;
	
	static
	{
		services = new ObjectMap<Class<? extends Service>, Service>();
	}
	

	/**
	 * Signs a service into the manager that can be injected into scenes
	 * @param cls
	 * @param service
	 */
	public static void register(Class<? extends Service> cls, Service service)
	{
		if (cls.isAssignableFrom(service.getClass()))
		{
			services.put(cls, service);
		}
		else
		{
			throw (new NullPointerException("Service registered is not of type specified: " + cls.getCanonicalName()));
		}
	}
	
	/**
	 * Injects an object's fields with various requested services
	 * @param o
	 */
	public static void inject(Object o)
	{
		Field[] fields = o.getClass().getFields();
		for (Field f : fields)
		{
			Inject anno = f.getAnnotation(Inject.class);
			if (anno != null && Service.class.isAssignableFrom(f.getType()))
			{
				f.setAccessible(true);
				@SuppressWarnings("unchecked")
				Class<? extends Service> type = (Class<? extends Service>) f.getType();
				try {
					Service service = services.get(type);
					f.set(o, type.cast(service));
					//System.out.println("Setting " + f.getName() + " to yeaha");
				} catch (IllegalArgumentException e){
					throw new NullPointerException("Service " + type.getCanonicalName() + " has not been registered into this system");
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Unhook dependencies from an Object
	 * @param o
	 */
	public static void unhook(Object o)
	{
		Field[] fields = o.getClass().getFields();
		for (Field f : fields)
		{
			Inject anno = f.getAnnotation(Inject.class);
			if (anno != null && Service.class.isAssignableFrom(f.getType()))
			{
				f.setAccessible(true);
				try {
					f.set(o, null);
					//System.out.println("Setting " + f.getName() + " to null");
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
