//"in" attributes from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec4 high;
uniform vec4 low;

uniform float contrast;

void main(void) {

    vec4 texCol = texture2D(u_texture, v_texCoords);
    vec3 h = vec3(0f);
    
    texCol *= v_color;
    
    if (any(greaterThan(texCol.rgb, h)))
    {
        texCol.rgb = mix(low.rgb, high.rgb, smoothstep(0, .5f, contrast));
        
    }
    else
    {  
        texCol.rgb = mix(low.rgb, high.rgb, smoothstep(.5f, 1f, contrast));
    }
    
    gl_FragColor = texCol;
}