void flw_transformBoundingSphere(in FlwInstance i, inout vec3 center, inout float radius) {
    mat4 pose = i.pose;
    center = (pose * vec4(center, 1.0)).xyz;

    float scale = max(length(pose[0].xyz), max(length(pose[1].xyz), length(pose[2].xyz)));
    radius *= scale;
}