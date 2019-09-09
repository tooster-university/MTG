package me.tooster.common;

import java.io.Serializable;

/**
 * Marker interface for defining models loaded from some serialized data source. Models should be validated on load.
 */
public interface Model extends Serializable {}
