package com.example.helper

fun Float?.getProductPrice(price : Float): Float{
    //this --> Percentage

    if(this == null)
        return price
    val remainingPricePercentage = this - 1f
    val priceAfterOffer = (remainingPricePercentage * price)/10

    return priceAfterOffer

}