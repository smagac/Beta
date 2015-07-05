//"in" attributes from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec4 high;
uniform vec4 low;

uniform float contrast;
uniform int vignette;


//Vignette addition from http://youtu.be/caQZKeAYgD8
uniform vec2 u_resolution;

const float outerRadius = .75, innerRadius = .25, intensity = .35;

void main(void) {

    vec4 texCol = texture2D(u_texture, v_texCoords);
    vec3 h = vec3(0);
    
    texCol *= v_color;
    
    if (any(greaterThan(texCol.rgb, h)))
    {
        texCol.rgb = mix(low.rgb, high.rgb, smoothstep(0.0, .5, contrast));
        
    }
    else
    {  
        texCol.rgb = mix(low.rgb, high.rgb, smoothstep(.5, 1.0, contrast));
        //throw in a vignette if enabled
        if (vignette == 1)
        {
            vec2 relativePosition = gl_FragCoord.xy / u_resolution - .5;
            // relativePosition.x *= u_resolution.x / u_resolution.y;
            float len = length(relativePosition);
            float v = smoothstep(outerRadius, innerRadius, len);
            texCol.rgb = mix(texCol.rgb, texCol.rgb * v, intensity);
        }
    }
    

    
    gl_FragColor = texCol;
}