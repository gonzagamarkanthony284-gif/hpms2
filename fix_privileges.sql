-- Fix privileges for hpmsuser on hpms2_db
USE mysql;
INSERT INTO db (Host, Db, User, Select_priv, Insert_priv, Update_priv, Delete_priv, 
                Create_priv, Drop_priv, Grant_priv, References_priv, Index_priv, 
                Alter_priv, Create_tmp_table_priv, Lock_tables_priv, Create_view_priv,
                Show_view_priv, Create_routine_priv, Alter_routine_priv, Execute_priv,
                Event_priv, Trigger_priv)
VALUES ('localhost', 'hpms2_db', 'hpmsuser', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'N', 'Y', 'Y', 
        'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y')
ON DUPLICATE KEY UPDATE
    Select_priv='Y', Insert_priv='Y', Update_priv='Y', Delete_priv='Y',
    Create_priv='Y', Drop_priv='Y', References_priv='Y', Index_priv='Y',
    Alter_priv='Y', Create_tmp_table_priv='Y', Lock_tables_priv='Y',
    Create_view_priv='Y', Show_view_priv='Y', Create_routine_priv='Y',
    Alter_routine_priv='Y', Execute_priv='Y', Event_priv='Y', Trigger_priv='Y';
