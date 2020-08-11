class TestObserver<T>(private val liveData: LiveData<T>) : Observer<T> {

//    init {
//        liveData.observeForever(this)
//    }

    private val testValues = mutableListOf<T>()

    override fun onChanged(t: T) {
        try {
            println("⏰ TestObserver onChanged()  testValues $testValues")
            if (t != null) testValues.add(t)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun testAssertNoValues(): TestObserver<T> {
        if (testValues.isNotEmpty()) throw AssertionException("Assertion error with actual size ${testValues.size}")
        return this
    }

    fun testAssertValueCount(count: Int): TestObserver<T> {
        if (count < 0) throw AssertionException("Assert count cannot be smaller than zero")
        if (count != testValues.size) throw AssertionException("Assertion error with expected $count while actual ${testValues.size}")
        return this
    }

    fun assertValues(vararg predicates: T): TestObserver<T> {
        predicates.forEach { predicate ->
            testValues.forEach { testValue ->
                if (predicate != testValue) throw  Exception("Assertion error")
            }
        }
        return this
    }

    fun assertValues(predicate: List<T>.() -> Boolean): TestObserver<T> {
        testValues.predicate()
        return this
    }

    fun values(predicate: List<T>.() -> Unit): TestObserver<T> {
        testValues.predicate()
        return this
    }

    fun values(): List<T> {
        return testValues
    }


    fun dispose() {
        testValues.clear()
        liveData.removeObserver(this)
    }
}


fun <T> LiveData<T>.test(): TestObserver<T> {

    val testObserver = TestObserver(this)

    observeForever(testObserver)

    return testObserver
}

class AssertionException(message: String) : Exception(message)
