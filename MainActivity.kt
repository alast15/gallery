package com.example.newulsuart

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url


class ArtsObj constructor(tit: String,artistTit:String,imageI:String){
    val title = tit
    val artistTitle = artistTit
    val imageId = imageI
}

interface ArticApiService {
    @GET
    fun getArtworks(@Url url:String): Call<JsonObject>
}


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomAdapter
    private var API_source = "https://api.artic.edu/api/v1/" //Основная ссылка для API
    private var limit = 25 // Количество элементов на одной странице
    private var page_count = 1 //Счётчик страниц


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Настройка RecyclerView
        recyclerView = findViewById(R.id.recycView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Обработчик кнопки перехода на следующую страницу
        val myB = findViewById<Button>(R.id.my_button)
        myB.setOnClickListener {
            page_count++
            updatePage(page_count)
        }

        // Обновление страницы
        updatePage(page_count)
    }

    private fun updatePage(pageCount: Int) {
        // Создание экземпляра Retrofit для запроса
        val retrofit = Retrofit.Builder()
            .baseUrl(API_source)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(ArticApiService::class.java)

        // Выполнение запроса к API
        service.getArtworks("artworks?fields=api_link,artist_title,id,image_id,title&page=$pageCount&limit=$limit")
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        // Получение данных из ответа
                        val data: JsonArray = response.body()?.getAsJsonArray("data") ?: JsonArray()
                        val itemsList = ArrayList<ArtsObj>()
                        // Запись элементов в массив данных
                        for (i in 0 until data.size()) {
                            try {
                                val artwork: JsonObject = data[i].asJsonObject
                                itemsList.add(
                                    ArtsObj(
                                        artwork.get("title").asString,
                                        artwork.get("artist_title").asString,
                                        artwork.get("image_id").asString
                                    )
                                )
                            } catch (e: Exception) {
                                // Псевдо Обработка ошибок
                            }
                        }

                        // Обновление адаптера и RecyclerView
                        adapter = CustomAdapter(itemsList, this@MainActivity)
                        recyclerView.adapter = adapter
                        //Обновление списка
                        adapter.notifyDataSetChanged()

                        // Обновление номера страницы
                        val cntView = findViewById<TextView>(R.id.countPage)
                        cntView.text = pageCount.toString()
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    // Обработка ошибок при запросе
                }
            })
    }

    class CustomAdapter(private val items: ArrayList<ArtsObj>, val cont: Context) :
        RecyclerView.Adapter<CustomAdapter.ArtViewHolder>() {
        class ArtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleTextView: TextView = itemView.findViewById(R.id.descriptionView)
            val artistTextView: TextView = itemView.findViewById(R.id.authorView)
            val imagineView: ImageView = itemView.findViewById(R.id.artImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtViewHolder {
            // Создание ViewHolder
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_element, parent, false)
            return ArtViewHolder(view)
        }

        override fun onBindViewHolder(holder: ArtViewHolder, position: Int) {
            val elementInArray = items[position]

            // Привязка данных к элементам ViewHolder
            holder.titleTextView.text = elementInArray.title //Обновление описания картины
            holder.artistTextView.text = elementInArray.artistTitle //Обновление автора картины
            Picasso.with(cont)
                .load("https://www.artic.edu/iiif/2/${elementInArray.imageId}/full/843,/0/default.jpg")
                .into(holder.imagineView) //Загружаем картину в ImageView

            //Добавления возможности открытия картины "во весь экран"
            holder.imagineView.setOnClickListener {
                val showBigImage = Dialog(cont, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                val imageView = ImageView(cont)
                Picasso.with(cont)
                    .load("https://www.artic.edu/iiif/2/${elementInArray.imageId}/full/843,/0/default.jpg")
                    .into(imageView)

                //Кнопка закрытия картины
                val closeButton = Button(cont)
                closeButton.text = "X"
                closeButton.width = 10
                closeButton.setTextColor(Color.WHITE)
                closeButton.backgroundTintList= ColorStateList.valueOf(Color.BLACK)
                closeButton.setOnClickListener { showBigImage.dismiss() }

                //Создание "экрана" и отображение его
                val layout = FrameLayout(cont)
                layout.addView(imageView,FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT))
                layout.addView(closeButton,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.END or Gravity.TOP))
                showBigImage.setContentView(layout)
                showBigImage.show()
            }
        }
        override fun getItemCount(): Int {
            return items.size
        }
    }
}



