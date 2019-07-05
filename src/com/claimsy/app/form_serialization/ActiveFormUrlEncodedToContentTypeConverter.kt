package com.claimsy.app.form_serialization

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.features.ContentConverter
import io.ktor.http.ContentType
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toByteArray
import kotlinx.coroutines.io.ByteReadChannel

class ActiveFormUrlEncodedToContentTypeConverter(val objectMapper: ObjectMapper) : ContentConverter {
    @UseExperimental(ExperimentalStdlibApi::class)
    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null

        val translator = FormParamsToJsonTranslator()

        val json = translator.jsonFromFormBody(channel.toByteArray().decodeToString())
        //Convert to json

        //TODO: eventually make this nonblocking
        return objectMapper.readValue(objectMapper.writeValueAsString(json), request.type.javaObjectType)
    }

    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: ContentType,
        value: Any
    ): Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
