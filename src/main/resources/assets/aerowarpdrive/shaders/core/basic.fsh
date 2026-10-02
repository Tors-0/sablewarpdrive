#version 150

out vec4 fragColor;

uniform float GameTime;

void main() {
    float pulse = 0.5 + 0.5 * sin(GameTime * 1000);
    vec3 finalColor = vec3(1.0, 0.0, 0.0) * pulse;
    fragColor = vec4(finalColor, 1.0);
}