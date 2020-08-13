# RxJava Style LiveData and Flow TestObserver
TestObserver class for LiveData to test multiple values like ViewState such as loading, and result states or multiple post and setValues

## LiveData

### Implementation

```
class TestObserver<T>(private val liveData: LiveData<T>) : Observer<T> {

//    init {
//        liveData.observeForever(this)
//    }

    private val testValues = mutableListOf<T>()

    override fun onChanged(t: T) {

        if (t != null) testValues.add(t)
        println("⏰ TestObserver onChanged()  testValues $testValues")

    }

    fun assertNoValues(): TestObserver<T> {
        if (testValues.isNotEmpty()) throw AssertionException("Assertion error with actual size ${testValues.size}")
        return this
    }

    fun assertValueCount(count: Int): TestObserver<T> {
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

class AssertionException(message: String) : Exception(message) {

}
```

## Usage

```
@Test
fun test() {

    // GIVEN
    val myTestData = MutableLiveData<Int>()
    val testObserver = myTestData.test()

    // WHEN
    myTestData.value = 1
    myTestData.value = 2
    myTestData.value = 3
    
    // THEN
    testObserver.assertValues {
        (this[0] == 1 && this[1] == 2 && this[2] == 3)

    }.assertValueCount(3)

    // 🔥 Do not forget to dispose
    testObserver.dispose()
}

```

## Flow

### Implementation

```

class FlowTestObserver<T>(
    coroutineScope: CoroutineScope,
    private val flow: Flow<T>
) {
    private val testValues = mutableListOf<T>()
    private var error: Throwable? = null

//    private val job: Job = coroutineScope.launch {
//        flow
//            .catch { throwable ->
//                error = throwable
//            }
//            .collect {
//                testValues.add(it)
//            }
//    }

    private val job: Job = flow
        .catch { throwable ->
            error = throwable
        }
        .onEach { testValues.add(it) }
        .launchIn(scope = coroutineScope)


    fun assertNoValues(): FlowTestObserver<T> {
        if (testValues.isNotEmpty()) throw AssertionError(
            "Assertion error! Actual size ${testValues.size}"
        )
        return this
    }

    fun assertValueCount(count: Int): FlowTestObserver<T> {
        if (count < 0) throw AssertionError(
            "Assertion error! Value count cannot be smaller than zero"
        )
        if (count != testValues.size) throw AssertionError(
            "Assertion error! Expected $count while actual ${testValues.size}"
        )
        return this
    }

    fun assertValues(vararg values: T): FlowTestObserver<T> {
        if (!testValues.containsAll(values.asList()))
            throw  AssertionError("Assertion error! At least one value does not match")
        return this
    }

//    fun assertValues(predicate: List<T>.() -> Boolean): FlowTestObserver<T> {
//        if (!testValues.predicate())
//            throw  AssertionError("Assertion error! At least one value does not match")
//        return this
//    }
//
//    fun values(predicate: List<T>.() -> Unit): FlowTestObserver<T> {
//        testValues.predicate()
//        return this
//    }

    fun assertValues(predicate: (List<T>) -> Boolean): FlowTestObserver<T> {
        if (!predicate(testValues))
            throw  AssertionError("Assertion error! At least one value does not match")
        return this
    }

    fun assertError(throwable: Throwable): FlowTestObserver<T> {

        val errorNotNull = exceptionNotNull()

        if (!(errorNotNull::class.java == throwable::class.java &&
                    errorNotNull.message == throwable.message)
        )
            throw AssertionError("Assertion Error! throwable: $throwable does not match $errorNotNull")

        return this
    }

    fun assertError(errorClass: Class<Throwable>): FlowTestObserver<T> {

        val errorNotNull = exceptionNotNull()

        if (errorNotNull::class.java != errorClass)
            throw  AssertionError("Assertion Error! errorClass $errorClass does not match ${errorNotNull::class.java}")

        return this
    }

    fun assertError(predicate: (Throwable) -> Boolean): FlowTestObserver<T> {

        val errorNotNull = exceptionNotNull()

        if (!predicate(errorNotNull))
            throw AssertionError("Assertion Error! Exception for $errorNotNull")

        return this
    }

    fun assertNull(): FlowTestObserver<T> {

        testValues.forEach {
            if (it != null) throw AssertionError("Assertion Error! There are more than one item that is not nuşş")

        }

        return this
    }

    fun values(predicate: (List<T>) -> Unit): FlowTestObserver<T> {
        predicate(testValues)
        return this
    }

    fun values(): List<T> {
        return testValues
    }

    private fun exceptionNotNull(): Throwable {

        if (error == null)
            throw  AssertionError("There is no exception")

        return error!!
    }

    fun dispose() {
        job.cancel()
    }
}

fun <T> Flow<T>.test(scope: CoroutineScope): FlowTestObserver<T> {
    return FlowTestObserver(scope, this)
}

```

## Usage
```
            postRemoteRepository.getPostFlow()
                .test(testCoroutineScope)
//                .assertError(Exception("Network Exception"))
                .assertError {
                    it.message == "Network Exception"
                }
                .dispose()
                
                
         testCoroutineScope.runBlockingTest {

            // GIVEN
            every { postDBRepository.getPostListFlow() } returns flow { emit(listOf()) }

            // WHEN
            val testObserver = postDBRepository.getPostListFlow().test(this)

            // THEN
            val actual = testObserver.values()[0]
            Truth.assertThat(actual.size).isEqualTo(0)
            testObserver.dispose()
        }
