def decidePipeline(configMap){
    type = configMap.get("type")

    switch(type) {
        case "nodejsEKS":
            nodejsEKS(configMap)
            break
        case "pythonEKS":
            pythonEKS(configMap)
            break
        case "javaEKS":
            javaEKS(configMap)
            break
        default:
            error "There is not matched library"
            break
    }
}