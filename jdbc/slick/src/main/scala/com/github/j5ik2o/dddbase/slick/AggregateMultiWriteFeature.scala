package com.github.j5ik2o.dddbase.slick

import com.github.j5ik2o.dddbase.AggregateMultiWriter
import monix.eval.Task

trait AggregateMultiWriteFeature extends AggregateMultiWriter[Task] with AggregateBaseWriteFeature {

  override def storeMulti(aggregates: Seq[AggregateType]): Task[Long] =
    for {
      records <- Task.traverse(aggregates) { aggregate =>
        convertToRecord(aggregate)
      }
      result <- Task.deferFutureAction { implicit ec =>
        import profile.api._
        db.run(DBIO.sequence(records.foldLeft(Seq.empty[DBIO[Long]]) {
            case (result, record) =>
              result :+ dao.insertOrUpdate(record).map(_.toLong)
          }))
          .map(_.sum)
      }
    } yield result
}
