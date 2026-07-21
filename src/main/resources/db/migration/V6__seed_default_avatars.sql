-- Seed default avatars for profile onboarding using Cloudinary provider
-- The public IDs (asset_keys) do not include folder names as they are served from Cloudinary root based on the URL analysis.

INSERT INTO media_assets (id, provider, asset_key, version, resource_type, format, collection) VALUES
('40000000-0000-0000-0000-000000000001'::uuid, 'CLOUDINARY', '64_d4fo1k_wnebqr', 1784619376, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000002'::uuid, 'CLOUDINARY', '78_jdqw4d_igelau', 1784619376, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000003'::uuid, 'CLOUDINARY', '53_kafxzv_qm9i2y', 1784619374, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000004'::uuid, 'CLOUDINARY', '45_pxz0fx_cbtmzz', 1784619374, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000005'::uuid, 'CLOUDINARY', '31_js95zg_egf5ae', 1784619373, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000006'::uuid, 'CLOUDINARY', '26_qse2gw_jgpexf', 1784619373, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000007'::uuid, 'CLOUDINARY', '22_umn6dl_vganab', 1784619372, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000008'::uuid, 'CLOUDINARY', '16_mrx1dn_wvbxb2', 1784619371, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000009'::uuid, 'CLOUDINARY', '08_tqar6z_nvbvsx', 1784619368, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000010'::uuid, 'CLOUDINARY', '02_aus2zc_tfpypg', 1784619368, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000011'::uuid, 'CLOUDINARY', '10_casfd5_o5c17e', 1784619368, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000012'::uuid, 'CLOUDINARY', '04_bhucyh_t0s4i9', 1784619368, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000013'::uuid, 'CLOUDINARY', '11_bb0em3_gsk14y', 1784619367, 'IMAGE', 'png', 'default_avatars'),
('40000000-0000-0000-0000-000000000014'::uuid, 'CLOUDINARY', '09_uvurms_b4gjxz', 1784619367, 'IMAGE', 'png', 'default_avatars');
