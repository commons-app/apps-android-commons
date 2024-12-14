package fr.free.nrw.commons.wikidata.model

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder

abstract class BaseModel {
    override fun toString(): String = ToStringBuilder.reflectionToString(this)

    override fun hashCode(): Int = HashCodeBuilder.reflectionHashCode(this)

    override fun equals(other: Any?): Boolean = EqualsBuilder.reflectionEquals(this, other)
}
