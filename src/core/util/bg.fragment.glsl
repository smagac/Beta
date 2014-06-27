//"in" attributes from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec4 hue;

void main(void) {
    gl_FragColor = texture2D(u_texture, v_texCoords) * v_color * hue;
}