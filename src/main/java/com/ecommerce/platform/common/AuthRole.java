package com.ecommerce.platform.common;

/**
 * Supported login identities. Each role is backed by its own database table.
 */
public enum AuthRole {
    ADMIN,
    BUYER,
    SELLER
}
