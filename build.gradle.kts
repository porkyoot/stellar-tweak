dependencies {
    implementation(project(":stellar-core"))
    compileOnly(project(":stellar-law"))
}

tasks.named<Jar>("jar") {
    from(project(":stellar-core").the<SourceSetContainer>()["main"].output)
}
