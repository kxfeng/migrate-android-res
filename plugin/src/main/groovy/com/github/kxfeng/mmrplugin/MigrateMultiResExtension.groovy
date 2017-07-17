package com.github.kxfeng.mmrplugin;

class MigrateMultiResExtension {
    List<MigrateMultiResSubTaskExtension> subTasks = []

    void methodMissing(String name, args) {
        if (args.length > 0 && args[0] instanceof Closure) {
            Closure closure = args[0]
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure.delegate = new MigrateMultiResSubTaskExtension(name)
            closure()

            subTasks.add(closure.delegate)
        }
    }
}

class MigrateMultiResSubTaskExtension {
    String name
    String from
    List<String> to

    MigrateMultiResSubTaskExtension(String name) {
        this.name = name
    }

    void from(String from) {
        this.from = from
    }

    void to(String[] to) {
        this.to = to
    }

    @Override
    String toString() {
        return "name=${name} from=${from} to=${to}"
    }
}
