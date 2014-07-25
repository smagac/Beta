package github.nhydock.ssm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injections are a simple annotation that allow you to take the main game handled by the SceneManager
 * and abstract it into a stricter limited "service" that's automatically thrown into the scene
 * at construction time by the manager.  Yes you'll be passing down the same reference but the advantage
 * is instead of seeing things as a single god object, you can now manage your processing in terms
 * of services that have a small feature set.  This can prevent you from making messing code where you
 * access everything from your main feature driver in a single class by helping you pin point exactly
 * what you should have per class.
 * <p/>
 * I mainly just made this because I'm not a fan of having 50 singletons that I have to remember to reset
 * whenever I restart my game without closing.  I will say it's also not a good idea to have a single class do 50
 * different things, as that can lead to a super large disgusting class that's hard to read and follow.
 * <p/>
 * Only use Injection and services in areas where it seems reasonable (ie. where there's a set amount of data
 * that you're going to be accessing a lot in a lot of different places)
 * @author nhydock
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject {

}
