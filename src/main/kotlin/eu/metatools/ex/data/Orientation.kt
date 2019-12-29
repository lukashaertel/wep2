package eu.metatools.ex.data

enum class Orientation {
    Up {
        override val dx = 0
        override val dy = 1
    },
    Right {
        override val dx = 1
        override val dy = 0
    },
    Down {
        override val dx = 0
        override val dy = -1
    },
    Left {
        override val dx = -1
        override val dy = 0
    };

    abstract val dx: Int
    abstract val dy: Int
}