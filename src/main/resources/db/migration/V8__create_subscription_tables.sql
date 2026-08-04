-- ==========================================
-- 2. QUẢN LÝ SUBSCRIPTION (BẢN PRO / MONETIZATION)
-- ==========================================

CREATE TABLE "subscription_plans" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
	"code" varchar(50) NOT NULL UNIQUE, -- 'PRO_MONTHLY', 'PRO_YEARLY', 'PRO_LIFETIME'
	"name" varchar(100) NOT NULL,
	"price_vnd" integer NOT NULL,
	"duration_days" integer, -- NULL nếu là Lifetime
	"is_active" boolean DEFAULT true,
	"created_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE "subscriptions" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
	"profile_id" uuid NOT NULL,
	"plan_id" uuid NOT NULL,
	"status" varchar(20) NOT NULL, -- 'ACTIVE', 'EXPIRED', 'CANCELLED', 'TRIAL'
	"starts_at" timestamp with time zone NOT NULL,
	"expires_at" timestamp with time zone, -- NULL nếu là Lifetime
	"payment_provider" varchar(30), -- 'APPLE_PAY', 'GOOGLE_PAY', 'STRIPE', 'MOMO', 'VNPAY'
	"external_transaction_id" varchar(255),
	"created_at" timestamp with time zone DEFAULT now(),
	"updated_at" timestamp with time zone DEFAULT now(),
	CONSTRAINT "subscriptions_profile_id_fkey" FOREIGN KEY ("profile_id") REFERENCES "profiles"("id") ON DELETE CASCADE,
	CONSTRAINT "subscriptions_plan_id_fkey" FOREIGN KEY ("plan_id") REFERENCES "subscription_plans"("id") ON DELETE RESTRICT,
	CONSTRAINT "chk_sub_status" CHECK (((status)::text = ANY (ARRAY['ACTIVE'::text, 'EXPIRED'::text, 'CANCELLED'::text, 'TRIAL'::text])))
);

CREATE INDEX "idx_subscriptions_profile_status" ON "subscriptions" ("profile_id", "status");

-- Seed some default subscription plans
INSERT INTO "subscription_plans" ("code", "name", "price_vnd", "duration_days") VALUES
('PRO_MONTHLY', 'Pro Monthly', 49000, 30),
('PRO_YEARLY', 'Pro Yearly', 399000, 365),
('PRO_LIFETIME', 'Pro Lifetime', 999000, NULL);
