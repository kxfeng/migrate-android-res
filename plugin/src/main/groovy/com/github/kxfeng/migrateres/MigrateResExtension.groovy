package com.github.kxfeng.migrateres;

class MigrateResExtension {
    Boolean enable = true
    List<MigrateResSubTaskExtension> subTasks = []

    void methodMissing(String name, args) {
        if (args.length > 0) {
            if (name.equalsIgnoreCase("enable")) {
                enable = args[0].toBoolean()
                return
            }

            if (args[0] instanceof Closure) {
                Closure closure = args[0]
                closure.resolveStrategy = Closure.DELEGATE_FIRST
                closure.delegate = new MigrateResSubTaskExtension(name)
                closure()

                subTasks.add(closure.delegate)
            }
        }
    }
}

class MigrateResSubTaskExtension {
    String name
    String from
    List<String> to

    MigrateResSubTaskExtension(String name) {
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
