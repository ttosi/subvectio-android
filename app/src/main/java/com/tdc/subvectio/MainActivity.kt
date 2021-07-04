package com.tdc.subvectio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tdc.subvectio.data.DeliveryDao
import com.tdc.subvectio.data.SubvectioDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var db: SubvectioDatabase
    private lateinit var ddao: DeliveryDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        db = SubvectioDatabase.getDatabase(this)
//        ddao = db.deliveryDao()


//
//
//
//        val d: com.tdc.subvectio.entities.Delivery = com.tdc.subvectio.entities.Delivery(
//            storeName = "Wendys",
//            storeAddress = "123 Anywhere Pl",
//            customerAddress = "456 Somewhere St",
//            distance = 12.345,
//            createdAt = LocalDateTime.now(),
//            acceptedAt = null
//        )
//
//        println("what in the the fuck cuck")
//
//        val offerButton: Button = findViewById(R.id.button1)
//        offerButton.setOnClickListener { dodb() }
//
////        ddao.insert(d)
////
//
//
////        val ds = withContext(Dispatchers.IO) {
////
////        }
//
////        println("------------------------------------------------------1")
////        runBlocking {
//////            ddao.insert(d)
////
////            var ds = ddao.list()
////            println(">>>>>>>>>>>>>>>>>>>> ${ds.first().storeName}")
////        }
////        println("------------------------------------------------------2")
//
////        suspend {
////            ddao.insert(d)
////            println("what????????????????????????")
////        }.run {  }
//    }
//
    }
    private fun dodb() {
        println("inside dodb...")
//        CoroutineScope(Dispatchers.Main).launch {
//            val profile = ddao.insert(d)
//            println(">>>>>>>>>>>>>>>>>>>> wtf wtf wtf")
//         val ds = ddao.list()
//        println(">>>>>>>>>> ${ds.count()}")
//        for(row in ds) {
//            Logger.log(row.storeName)
//        }




//            var fir = ds.first()
//            println(">dd>>>>>>>dff>>> ${ds.count()} ${fir.storeName} and bp = ${fir.basePay}")
//
////
//            fir.basePay = 799.65
//            ddao.update(fir)
        }
}
