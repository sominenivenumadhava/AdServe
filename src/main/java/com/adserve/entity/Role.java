package com.adserve.entity;

/**
 * Roles for platform access control.
 * ADMIN has overarching permissions across all advertisers and campaigns.
 * ADVERTISER is restricted to resources and analytics belonging to their own account.
 */
public enum Role {
    ADMIN,
    ADVERTISER
}
