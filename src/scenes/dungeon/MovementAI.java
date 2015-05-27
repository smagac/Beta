package scenes.dungeon;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.MathUtils;

import core.components.Combat;
import core.components.Position;

enum MovementAI implements State<Entity> {
    Wander {

        @Override
        public void update(Entity entity) {
            Combat combat = Combat.Map.get(entity);
            Position m = Position.Map.get(entity);
            Position p = Position.Map.get(world.player);

            // roll for move
            // Stats s = statMap.get(e);
            if (MathUtils.randomBoolean(combat.getMovementRate(false))) {

                int[] dest = { m.getX(), m.getY() };

                Direction dir = Direction.values()[MathUtils.random(Direction.values().length-1)];
                dir.move(dest);

                // follow player chance
                world.moveEntity(dest[0], dest[1], entity);

                if (p.distance(m) < 3) {
                    combat.getAI().changeState(Agro);
                }
            }
        }

    },
    Passive {

        @Override
        public void update(Entity entity) {
            Combat combat = Combat.Map.get(entity);
            Position m = Position.Map.get(entity);

            // roll for move
            // Stats s = statMap.get(e);
            if (MathUtils.randomBoolean(combat.getMovementRate(false))) {

                int[] dest = { m.getX(), m.getY() };

                Direction dir = Direction.values()[MathUtils.random(Direction.values().length-1)];
                dir.move(dest);

                // follow player chance
                world.moveEntity(dest[0], dest[1], entity);
            }
        }

    },
    Agro {

        @Override
        public void update(Entity entity) {
            Combat combat = Combat.Map.get(entity);
            Position m = Position.Map.get(entity);
            Position p = Position.Map.get(world.player);

            if (MathUtils.randomBoolean(combat.getMovementRate(true))) {

                int dX = 0;
                int dY = 0;
                boolean priority = MathUtils.randomBoolean();

                // horizontal priority flip
                if (priority) {
                    if (p.getX() < m.getX())
                        dX = -1;
                    if (p.getX() > m.getX())
                        dX = 1;
                    if (dX == 0) {
                        if (p.getY() < m.getY())
                            dY = -1;
                        if (p.getY() > m.getY())
                            dY = 1;
                    }

                }
                // vertical priority
                else {
                    if (p.getY() < m.getY())
                        dY = -1;
                    if (p.getY() > m.getY())
                        dY = 1;
                    if (dY == 0) {
                        if (p.getX() < m.getX())
                            dX = -1;
                        if (p.getX() > m.getX())
                            dX = 1;
                    }
                }

                // follow player chance
                world.moveEntity(m.getX() + dX, m.getY() + dY, entity);

                if (p.distance(m) > 5) {
                    combat.getAI().changeState(Wander);
                }

            }
        }

    };

    protected static MovementSystem world;

    @Override
    public void enter(Entity entity) {
    }

    @Override
    public void exit(Entity entity) {
    }

    @Override
    public boolean onMessage(Entity entity, Telegram telegram) {
        return false;
    }

    protected static void setWorld(MovementSystem w) {
        world = w;
    }
}