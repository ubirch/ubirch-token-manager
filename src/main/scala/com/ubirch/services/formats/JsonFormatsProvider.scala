package com.ubirch.services.formats

import com.ubirch.models.{ TokenClaimSerializer, TokenPurposedClaimSerializer }
import org.json4s.ext.{ JavaTypesSerializers, JodaTimeSerializers }
import org.json4s.{ DefaultFormats, Formats }
import javax.inject._

trait BaseFormats {
  val baseFormats: Formats = DefaultFormats.lossless ++ JavaTypesSerializers.all ++ JodaTimeSerializers.all
}

trait CustomFormats extends BaseFormats {
  val formats: Formats = baseFormats ++ List(new TokenClaimSerializer()(baseFormats)) ++ List(new TokenPurposedClaimSerializer()(baseFormats))
}

/**
  * Represents a Json Formats Provider
  */
@Singleton
class JsonFormatsProvider extends Provider[Formats] with CustomFormats {
  override def get(): Formats = formats
}
