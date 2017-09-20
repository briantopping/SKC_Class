package com.developintelligence.skc.consumer

import com.datastax.driver.core.Session

object Statements extends Serializable {

  def cql(id: Long, event: Array[Byte]): String = s"""
       insert into my_keyspace.test_table (id, event)
       values('$id', '$event event')"""

  def createKeySpaceAndTable(session: Session, dropTable: Boolean = false) = {
    session.execute(
      """CREATE KEYSPACE  if not exists  my_keyspace WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };""")
    if (dropTable)
      session.execute("""drop table if exists my_keyspace.test_table""")

    session.execute(
      """create table if not exists my_keyspace.test_table ( id long, event blob, primary key(id) )""")
  }
}
