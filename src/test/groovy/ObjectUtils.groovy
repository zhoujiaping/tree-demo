class ObjectUtils {
    def static deepClone(obj){
        def bytesOutput = new ByteArrayOutputStream()
        def output = new ObjectOutputStream(bytesOutput)
        output.writeObject(obj)
        def input = new ObjectInputStream(new ByteArrayInputStream(bytesOutput.toByteArray()))
        input.readObject()
    }
}
