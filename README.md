# RxJava-Style-LiveData-TestObserver
TestObserver class for LiveData to test multiple values like ViewState such as loading, and result states or multiple post and setValues


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
