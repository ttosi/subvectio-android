const fs = require('fs');

fs.readFile('./activity_20210705.log', 'utf8', (err, data) => {
  // console.log(data)

  const offers = data.split(/\[.*\] xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/)
  console.log(offers.length)

  for(i in offers) {
    var rows = offers[i].split('\n')
    // console.log(offers[i])


    const isDeclined = offers[i].search('DECLINED') >= 0
    console.log(isDeclined)

    // console.log(rows)
    // console.log(rows[2])

    // const storeName = rows[2].match(/\[.*\] (.*)/)
    // console.log(storeName[1])

    // const isDeclined = rows[13].match(/\[.*\] (\>\>\> DECLINED)/)
    // console.log(isDeclined)
  }
})