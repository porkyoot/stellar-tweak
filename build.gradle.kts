dependencies {
    implementation(project(":stellar-core"))
    compileOnly(project(":stellar-law"))
    compileOnly("com.terraformersmc:modmenu:${project.property("modmenu_version")}")
}

tasks.named<Jar>("jar") {
    from(project(":stellar-core").the<SourceSetContainer>()["main"].output)
}
