package scene2d.ui.extras;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class ParticleActor extends Actor {

    public static class ResetParticle implements Runnable {
        private ParticleActor p;
        
        public ResetParticle(ParticleActor p) {
            this.p = p;
        }
        
        @Override
        public void run() {
            p.particle.reset();
            p.particle.start();
        }
    }
    
    ParticleEffect particle;
    
    public ParticleActor(ParticleEffect p) {
        particle = p;
    }
    
    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        
        particle.setPosition(x, y);
    }
    
    public void setParticleEffect(ParticleEffect p) {
        particle = p;
        p.reset();
        p.start();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (particle != null)
            particle.update(delta);
    }
    
    @Override
    public void draw(Batch batch, float parentAlpha) {

        if (particle != null && !particle.isComplete()) {
            particle.draw(batch);
        }
        
    }
    
}
